import React, { useState, useCallback } from 'react';
import { Search, Plus, Edit, Eye, EyeOff, Trash2, X } from 'lucide-react';

const ProductManagement = () => {
    const [activeTab, setActiveTab] = useState('category');
    const [isDrawerOpen, setIsDrawerOpen] = useState(false);
    const [editingItem, setEditingItem] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');

    // Sample data for different tabs
    const [data, setData] = useState({
        category: [
            { id: 1, code: 'DM001', name: 'Áo thun', description: 'Danh mục áo thun nam nữ', status: true },
            { id: 2, code: 'DM002', name: 'Quần jean', description: 'Danh mục quần jean cao cấp', status: true },
            { id: 3, code: 'DM003', name: 'Giày sneaker', description: 'Danh mục giày thể thao', status: false },
        ],
        brand: [
            { id: 1, code: 'HG001', name: 'Nike', description: 'Thương hiệu thể thao nổi tiếng', status: true },
            { id: 2, code: 'HG002', name: 'Adidas', description: 'Thương hiệu thời trang thể thao', status: true },
            { id: 3, code: 'HG003', name: 'Uniqlo', description: 'Thương hiệu thời trang Nhật Bản', status: false },
        ],
        material: [
            { id: 1, code: 'CL001', name: 'Cotton 100%', description: 'Chất liệu cotton tự nhiên', status: true },
            { id: 2, code: 'CL002', name: 'Polyester', description: 'Chất liệu tổng hợp bền đẹp', status: true },
            { id: 3, code: 'CL003', name: 'Denim', description: 'Chất liệu jean truyền thống', status: false },
        ],
        size: [
            { id: 1, code: 'S001', name: 'S', description: 'Size nhỏ (Small)', status: true },
            { id: 2, code: 'S002', name: 'M', description: 'Size trung bình (Medium)', status: true },
            { id: 3, code: 'S003', name: 'L', description: 'Size lớn (Large)', status: true },
            { id: 4, code: 'S004', name: 'XL', description: 'Size rất lớn (Extra Large)', status: false },
        ],
        color: [
            { id: 1, code: 'MS001', name: 'Đen', description: 'Màu đen cơ bản', status: true },
            { id: 2, code: 'MS002', name: 'Trắng', description: 'Màu trắng tinh khôi', status: true },
            { id: 3, code: 'MS003', name: 'Xanh navy', description: 'Màu xanh navy sang trọng', status: false },
        ],
    });

    const [formData, setFormData] = useState({
        code: '',
        name: '',
        description: '',
        status: true
    });

    const tabs = [
        { key: 'category', label: 'Danh mục' },
        { key: 'brand', label: 'Hãng' },
        { key: 'material', label: 'Chất liệu' },
        { key: 'size', label: 'Size' },
        { key: 'color', label: 'Màu' },
    ];

    const currentData = data[activeTab] || [];
    const filteredData = currentData.filter(item =>
        item.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.code.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const handleAdd = () => {
        setEditingItem(null);
        setFormData({ code: '', name: '', description: '', status: true });
        setIsDrawerOpen(true);
    };

    const handleEdit = (item) => {
        setEditingItem(item);
        setFormData(item);
        setIsDrawerOpen(true);
    };

    const handleSave = () => {
        if (!formData.name.trim() || !formData.code.trim()) {
            alert('Vui lòng nhập đầy đủ mã và tên!');
            return;
        }

        const newData = { ...data };

        if (editingItem) {
            // Update existing item
            const index = newData[activeTab].findIndex(item => item.id === editingItem.id);
            if (index !== -1) {
                newData[activeTab][index] = { ...formData, id: editingItem.id };
            }
        } else {
            // Add new item
            const maxId = Math.max(...newData[activeTab].map(item => item.id), 0);
            const newItem = { ...formData, id: maxId + 1 };
            newData[activeTab].push(newItem);
        }

        setData(newData);
        setIsDrawerOpen(false);
        setFormData({ code: '', name: '', description: '', status: true });
        setEditingItem(null);
    };

    const handleToggleStatus = (id) => {
        const newData = { ...data };
        const item = newData[activeTab].find(item => item.id === id);
        if (item) {
            item.status = !item.status;
            setData(newData);
        }
    };

    const handleDelete = (id) => {
        if (confirm('Bạn có chắc chắn muốn xóa mục này?')) {
            const newData = { ...data };
            newData[activeTab] = newData[activeTab].filter(item => item.id !== id);
            setData(newData);
        }
    };

    const closeDrawer = () => {
        setIsDrawerOpen(false);
        setEditingItem(null);
        setFormData({ code: '', name: '', description: '', status: true });
    };

    return (
        <div className="min-h-screen bg-gray-50 p-6">
            <div className="max-w-6xl mx-auto">
                <h1 className="text-2xl font-bold text-gray-900 mb-6">Quản lý sản phẩm</h1>

                {/* Tabs */}
                <div className="bg-white rounded-lg shadow mb-6">
                    <div className="border-b border-gray-200">
                        <nav className="flex space-x-8 px-6" aria-label="Tabs">
                            {tabs.map((tab) => (
                                <button
                                    key={tab.key}
                                    onClick={() => setActiveTab(tab.key)}
                                    className={`py-4 px-1 border-b-2 font-medium text-sm ${
                                        activeTab === tab.key
                                            ? 'border-blue-500 text-blue-600'
                                            : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                                    }`}
                                >
                                    {tab.label}
                                </button>
                            ))}
                        </nav>
                    </div>

                    {/* Actions Bar */}
                    <div className="p-6 border-b border-gray-200">
                        <div className="flex justify-between items-center">
                            <button
                                onClick={handleAdd}
                                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                            >
                                <Plus className="w-4 h-4 mr-2" />
                                Thêm
                            </button>

                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Search className="h-4 w-4 text-gray-400" />
                                </div>
                                <input
                                    type="text"
                                    placeholder="Search…"
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                    className="block w-64 pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-blue-500 focus:border-blue-500"
                                />
                            </div>
                        </div>
                    </div>

                    {/* Table */}
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Mã</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tên</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Mô tả</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Trạng thái</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Thao tác</th>
                            </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                            {filteredData.map((item) => (
                                <tr key={item.id} className="hover:bg-gray-50">
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                        {item.code}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                                        {item.name}
                                    </td>
                                    <td className="px-6 py-4 text-sm text-gray-900 max-w-xs truncate">
                                        {item.description}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                          item.status
                              ? 'bg-green-100 text-green-800'
                              : 'bg-red-100 text-red-800'
                      }`}>
                        {item.status ? 'Hiện' : 'Ẩn'}
                      </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                                        <div className="flex space-x-2">
                                            <button
                                                onClick={() => handleEdit(item)}
                                                className="text-blue-600 hover:text-blue-900 p-1 rounded hover:bg-blue-50"
                                                title="Sửa"
                                            >
                                                <Edit className="w-4 h-4" />
                                            </button>
                                            <button
                                                onClick={() => handleToggleStatus(item.id)}
                                                className={`p-1 rounded ${
                                                    item.status
                                                        ? 'text-orange-600 hover:text-orange-900 hover:bg-orange-50'
                                                        : 'text-green-600 hover:text-green-900 hover:bg-green-50'
                                                }`}
                                                title={item.status ? 'Ẩn' : 'Hiện'}
                                            >
                                                {item.status ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                            </button>
                                            <button
                                                onClick={() => handleDelete(item.id)}
                                                className="text-red-600 hover:text-red-900 p-1 rounded hover:bg-red-50"
                                                title="Xóa"
                                            >
                                                <Trash2 className="w-4 h-4" />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>

                        {filteredData.length === 0 && (
                            <div className="text-center py-12">
                                <div className="text-gray-400 text-lg">Không có dữ liệu</div>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Drawer Form */}
            {isDrawerOpen && (
                <div className="fixed inset-0 overflow-hidden z-50">
                    <div className="absolute inset-0 overflow-hidden">
                        <div className="absolute inset-0 bg-gray-500 bg-opacity-75 transition-opacity" onClick={closeDrawer}></div>
                        <div className="fixed inset-y-0 right-0 pl-10 max-w-full flex">
                            <div className="w-screen max-w-md">
                                <div className="h-full flex flex-col bg-white shadow-xl">
                                    <div className="flex-1 py-6 overflow-y-auto">
                                        <div className="px-4 sm:px-6">
                                            <div className="flex items-start justify-between">
                                                <h2 className="text-lg font-medium text-gray-900">
                                                    {editingItem ? 'Cập nhật' : 'Thêm mới'}
                                                </h2>
                                                <div className="ml-3 h-7 flex items-center">
                                                    <button
                                                        onClick={closeDrawer}
                                                        className="bg-white rounded-md text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                                    >
                                                        <X className="h-6 w-6" />
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                        <div className="mt-6 relative flex-1 px-4 sm:px-6">
                                            <div className="space-y-6">
                                                <div>
                                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                                        Mã *
                                                    </label>
                                                    <input
                                                        type="text"
                                                        value={formData.code}
                                                        onChange={(e) => setFormData({...formData, code: e.target.value})}
                                                        className="block w-full border border-gray-300 rounded-md px-3 py-2 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                                        placeholder="Nhập mã"
                                                    />
                                                </div>

                                                <div>
                                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                                        Tên *
                                                    </label>
                                                    <input
                                                        type="text"
                                                        value={formData.name}
                                                        onChange={(e) => setFormData({...formData, name: e.target.value})}
                                                        className="block w-full border border-gray-300 rounded-md px-3 py-2 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                                        placeholder="Nhập tên"
                                                    />
                                                </div>

                                                <div>
                                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                                        Mô tả
                                                    </label>
                                                    <textarea
                                                        rows={4}
                                                        value={formData.description}
                                                        onChange={(e) => setFormData({...formData, description: e.target.value})}
                                                        className="block w-full border border-gray-300 rounded-md px-3 py-2 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                                        placeholder="Nhập mô tả"
                                                    />
                                                </div>

                                                <div className="flex items-center">
                                                    <label className="flex items-center cursor-pointer">
                                                        <input
                                                            type="checkbox"
                                                            checked={formData.status}
                                                            onChange={(e) => setFormData({...formData, status: e.target.checked})}
                                                            className="sr-only"
                                                        />
                                                        <div className={`relative w-11 h-6 rounded-full transition-colors duration-200 ease-in-out ${
                                                            formData.status ? 'bg-blue-600' : 'bg-gray-200'
                                                        }`}>
                                                            <div className={`absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform duration-200 ease-in-out ${
                                                                formData.status ? 'translate-x-5' : 'translate-x-0'
                                                            }`}></div>
                                                        </div>
                                                        <span className="ml-3 text-sm font-medium text-gray-700">
                              Trạng thái hoạt động
                            </span>
                                                    </label>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div className="flex-shrink-0 px-4 py-4 flex justify-end space-x-3 border-t border-gray-200">
                                        <button
                                            onClick={closeDrawer}
                                            className="py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                                        >
                                            Hủy
                                        </button>
                                        <button
                                            onClick={handleSave}
                                            className="py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                                        >
                                            Lưu
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ProductManagement;